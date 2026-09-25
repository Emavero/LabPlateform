package com.labplatform.adapter.out.hypervisor.docker;

import java.time.Duration;
import java.util.List;

/** Exécute une commande système. Abstrait pour pouvoir tester l'adaptateur Docker sans Docker. */
public interface CommandRunner {

    /**
     * @param command  programme et arguments, jamais interprétés par un shell
     * @param stdin    texte envoyé sur l'entrée standard, ou null
     * @param timeout  durée maximale ; au-delà, le processus est tué
     */
    Result run(List<String> command, String stdin, Duration timeout);

    record Result(int exitCode, String stdout, String stderr) {

        public static final int TIMED_OUT = -1;

        public boolean succeeded() {
            return exitCode == 0;
        }
    }
}
