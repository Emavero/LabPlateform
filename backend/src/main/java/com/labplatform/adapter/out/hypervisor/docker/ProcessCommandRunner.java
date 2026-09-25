package com.labplatform.adapter.out.hypervisor.docker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Exécution réelle via ProcessBuilder : arguments passés tels quels, sans shell. */
public final class ProcessCommandRunner implements CommandRunner {

    @Override
    public Result run(List<String> command, String stdin, Duration timeout) {
        Process process;
        try {
            process = new ProcessBuilder(command).start();
        } catch (IOException e) {
            return new Result(127, "", "Commande introuvable ou non exécutable : " + command.get(0));
        }
        // Les deux flux sont lus en parallèle pour qu'un tampon plein ne bloque jamais le processus.
        CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
        CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));
        try (OutputStream in = process.getOutputStream()) {
            if (stdin != null) {
                in.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // Le processus a pu se terminer avant de lire son entrée : son code de sortie le dira.
        }
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return new Result(Result.TIMED_OUT, "", "Délai dépassé après " + timeout.toSeconds() + " s");
            }
            return new Result(process.exitValue(), out.join(), err.join());
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return new Result(Result.TIMED_OUT, "", "Commande interrompue");
        }
    }

    private static String readAll(InputStream stream) {
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
