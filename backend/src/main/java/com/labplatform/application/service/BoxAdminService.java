package com.labplatform.application.service;

import com.labplatform.application.port.in.admin.BoxDraft;
import com.labplatform.application.port.in.admin.ManageBoxesUseCase;
import com.labplatform.application.port.in.admin.PublishedBox;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.shared.Slug;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.AdminPolicy;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Administration du catalogue de machines.
 * <p>
 * Les flags sont le point délicat. Ils ne sont jamais relus : seule leur
 * empreinte est conservée. À la création, faute de valeur fournie, ils sont
 * tirés au hasard et renvoyés **une seule fois**, pour que l'administrateur
 * puisse les déposer sur la cible. À la modification, un champ vide veut dire
 * « ne change rien », pas « efface ».
 */
public class BoxAdminService implements ManageBoxesUseCase {

    private static final Comparator<Box> DISPLAY_ORDER =
            Comparator.comparing(Box::getReleasedAt).reversed().thenComparing(Box::getName);
    private static final int MAX_SYNOPSIS_LENGTH = 512;

    private final BoxRepositoryPort boxes;
    private final SecretGeneratorPort secrets;
    private final TransactionPort transactions;
    private final Clock clock;

    public BoxAdminService(BoxRepositoryPort boxes, SecretGeneratorPort secrets, TransactionPort transactions,
                           Clock clock) {
        this.boxes = boxes;
        this.secrets = secrets;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<Box> listBoxes(Actor actor) {
        AdminPolicy.requireAdmin(actor);
        return boxes.findAll().stream().sorted(DISPLAY_ORDER).toList();
    }

    @Override
    public PublishedBox createBox(Actor actor, BoxDraft draft) {
        AdminPolicy.requireAdmin(actor);
        requireWellFormed(draft);

        String slug = Slug.from(draft.name());
        String userSecret = flagOrGenerated(draft.userFlag());
        String rootSecret = flagOrGenerated(draft.rootFlag());

        Box saved = transactions.inTransaction(() -> {
            if (boxes.findBySlug(slug).isPresent()) {
                throw new ConflictException("Une machine porte déjà ce nom");
            }
            return boxes.save(Box.create(slug, draft.name().trim(), draft.operatingSystem(), draft.difficulty(),
                    synopsisOf(draft), draft.ipAddress().trim(), makerOf(draft), clock.instant(), draft.retired(),
                    draft.proOnly(), Flag.ofSecret(userSecret), Flag.ofSecret(rootSecret)));
        });
        return new PublishedBox(saved, userSecret, rootSecret);
    }

    @Override
    public PublishedBox updateBox(Actor actor, String slug, BoxDraft draft) {
        AdminPolicy.requireAdmin(actor);
        requireWellFormed(draft);

        String userSecret = normalized(draft.userFlag());
        String rootSecret = normalized(draft.rootFlag());

        Box saved = transactions.inTransaction(() -> {
            Box existing = require(slug);
            return boxes.save(Box.restore(existing.getId(), existing.getSlug(), draft.name().trim(),
                    draft.operatingSystem(), draft.difficulty(), synopsisOf(draft), draft.ipAddress().trim(),
                    makerOf(draft), existing.getReleasedAt(), draft.retired(), draft.proOnly(),
                    userSecret == null ? existing.getUserFlag() : Flag.ofSecret(userSecret),
                    rootSecret == null ? existing.getRootFlag() : Flag.ofSecret(rootSecret)));
        });
        return new PublishedBox(saved, userSecret, rootSecret);
    }

    @Override
    public void deleteBox(Actor actor, String slug) {
        AdminPolicy.requireAdmin(actor);
        // Les validations, notes et instances tombent avec la machine (ON DELETE CASCADE).
        transactions.inTransaction(() -> boxes.delete(require(slug)));
    }

    /** Un flag fourni doit être bien formé ; vide, il est tiré au hasard. */
    private String flagOrGenerated(String submitted) {
        String normalized = normalized(submitted);
        return normalized == null ? secrets.hexToken() : normalized;
    }

    private static String normalized(String submitted) {
        return submitted == null || submitted.isBlank() ? null : Flag.requireWellFormed(submitted);
    }

    private static void requireWellFormed(BoxDraft draft) {
        if (draft.ipAddress() == null || draft.ipAddress().isBlank()) {
            throw new InvalidInputException("L'adresse de la machine est obligatoire");
        }
        if (draft.synopsis() != null && draft.synopsis().length() > MAX_SYNOPSIS_LENGTH) {
            throw new InvalidInputException("Le synopsis est limité à " + MAX_SYNOPSIS_LENGTH + " caractères");
        }
    }

    private static String synopsisOf(BoxDraft draft) {
        return draft.synopsis() == null ? "" : draft.synopsis().trim();
    }

    private static String makerOf(BoxDraft draft) {
        return draft.maker() == null || draft.maker().isBlank() ? "cyberMans" : draft.maker().trim();
    }

    private Box require(String slug) {
        return boxes.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Machine introuvable"));
    }
}
