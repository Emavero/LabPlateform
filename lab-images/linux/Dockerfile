# Image des machines Linux de lab (mode APP_HYPERVISOR_MODE=docker).
# Chaque démarrage crée un conteneur neuf à partir de cette image ; l'arrêt le
# supprime. Le mot de passe temporaire est injecté par le backend après le
# démarrage (docker exec + chpasswd) : il n'apparaît ni dans l'image, ni dans
# les variables d'environnement du conteneur.
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      openssh-server sudo bash-completion less nano vim-tiny \
      curl wget ca-certificates iputils-ping iproute2 net-tools dnsutils \
      netcat-openbsd nmap tcpdump python3 procps \
 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /run/sshd \
 # Clés d'hôte générées une fois dans l'image : l'empreinte reste identique
 # d'un démarrage à l'autre, le client SSH ne signale donc pas de changement.
 && ssh-keygen -A

COPY lab.conf /etc/ssh/sshd_config.d/00-lab.conf
COPY entrypoint.sh /usr/local/bin/lab-entrypoint
RUN chmod 0644 /etc/ssh/sshd_config.d/00-lab.conf && chmod 0755 /usr/local/bin/lab-entrypoint

EXPOSE 22
ENTRYPOINT ["/usr/local/bin/lab-entrypoint"]
