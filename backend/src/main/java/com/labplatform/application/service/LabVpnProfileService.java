package com.labplatform.application.service;

import com.labplatform.application.port.in.vpn.LabVpnProfileSummary;
import com.labplatform.application.port.in.vpn.ManageLabVpnProfileUseCase;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.LabVpnProfilePort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.AdminPolicy;
import com.labplatform.domain.vpn.LabNetwork;
import com.labplatform.domain.vpn.LabVpnProfile;

import java.time.Clock;
import java.util.Optional;

/**
 * Le profil VPN que l'administration met à disposition.
 * <p>
 * Le service ne valide rien lui-même : c'est {@link LabVpnProfile} qui refuse
 * ce qui n'est pas un profil client, et le domaine est le bon endroit pour
 * cela. Ce qui se joue ici, c'est le droit d'agir et la trace qu'on en garde.
 */
public class LabVpnProfileService implements ManageLabVpnProfileUseCase {

    private final LabVpnProfilePort profiles;
    private final JournalPort journal;
    private final Clock clock;
    private final String labNetwork;

    public LabVpnProfileService(LabVpnProfilePort profiles, JournalPort journal, Clock clock, String labNetwork) {
        this.profiles = profiles;
        this.journal = journal;
        this.clock = clock;
        this.labNetwork = labNetwork;
    }

    @Override
    public LabVpnProfileSummary upload(Actor actor, String fileName, String content) {
        AdminPolicy.requireAdmin(actor);
        LabVpnProfile profile = LabVpnProfile.of(fileName, content, clock.instant());
        profiles.save(profile);
        journal.record(JournalEvent.of(actor.userId(), JournalKind.VPN_PROFILE_ISSUED, profile.fileName(),
                clock.instant()));
        return summaryOf(profile);
    }

    @Override
    public Optional<LabVpnProfileSummary> current(Actor actor) {
        AdminPolicy.requireAdmin(actor);
        return profiles.find().map(this::summaryOf);
    }

    @Override
    public void remove(Actor actor) {
        AdminPolicy.requireAdmin(actor);
        profiles.delete();
    }

    private LabVpnProfileSummary summaryOf(LabVpnProfile profile) {
        return new LabVpnProfileSummary(profile.fileName(), profile.sizeBytes(), profile.uploadedAt(),
                profile.routes(LabNetwork.ofCidr(labNetwork)));
    }
}
