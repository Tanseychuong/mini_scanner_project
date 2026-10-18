# MiniScanner sandbox image: compiles the source and ships the OS tools the modules wrap
# (Java 22+ is needed for the Foreign Function & Memory API used by the packet sniffer)
FROM eclipse-temurin:25-jdk-alpine

# libpcap for the packet sniffer; tcpdump is only there to cross-check its output
RUN apk add --no-cache traceroute net-tools iproute2 iputils libpcap tcpdump

WORKDIR /app
COPY src/ src/
RUN javac -d classes src/*.java

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-cp", "/app/classes", "NetworkManager"]
