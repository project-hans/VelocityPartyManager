FROM itzg/mc-proxy
LABEL authors="Caesar"

# Server config
ENV EULA=TRUE
ENV TYPE=VELOCITY
ENV VELOCITY_VERSION=3.3.0-SNAPSHOT
ENV MINECRAFT_VERSION=1.21.4

ADD build/libs/VelocityPartyManager-1.0.0-SNAPSHOT-all.jar /server/plugins/VelocityPartyManager-1.0-SNAPSHOT.jar
ADD tests/forwarding.secret /server/forwarding.secret
ADD tests/velocity.toml /server/velocity.toml
