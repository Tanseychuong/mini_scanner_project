# MiniScanner v1.0

```
███╗   ███╗██╗███╗   ██╗██╗███████╗ ██████╗ █████╗ ███╗   ██╗
████╗ ████║██║████╗  ██║██║██╔════╝██╔════╝██╔══██╗████╗  ██║
██╔████╔██║██║██╔██╗ ██║██║███████╗██║     ███████║██╔██╗ ██║
██║╚██╔╝██║██║██║╚██╗██║██║╚════██║██║     ██╔══██║██║╚██╗██║
██║ ╚═╝ ██║██║██║ ╚████║██║███████║╚██████╗██║  ██║██║ ╚████║
╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝╚═╝╚══════╝ ╚═════╝╚═╝  ╚═╝╚═╝  ╚═══╝

                 MiniScanner v1.0
          Java Network Discovery Utility
```

## Overview

MiniScanner is a Java-based network analysis tool designed to explore fundamental networking concepts such as host discovery, port scanning, and system network information gathering.

The project was built as a learning project to understand how tools like professional network scanners work internally using Java networking APIs.

> ⚠️ Use this tool only on networks and devices you own or have explicit permission to test.

---

## Features

```
+--------------------------------+
|        MiniScanner             |
+--------------------------------+
| 1. Network Information         |
|    - Network interfaces        |
|    - IP addresses              |
|                                |
| 2. Host Discovery              |
|    - Finds reachable devices   |
|    - Local subnet scanning     |
|                                |
| 3. Port Scanner                |
|    - TCP port checking         |
|    - Service identification    |
|                                |
| 4. WiFi Information            |
|    - Available networks        |
|    - Network details           |
+--------------------------------+
```

### Current capabilities

* Display network interfaces, MAC addresses and IP/prefix
* Automatic local subnet detection (or enter any CIDR from /20 to /32)
* Multithreaded host discovery (ICMP with TCP fallback, reverse DNS names)
* Multithreaded TCP port scanning: `common`, ranges (`1-1024`) or lists (`22,80,443`)
* Service banner grabbing (SSH/FTP/SMTP banners, HTTP `Server` header)
* JSON/CSV reports saved to `reports/`
* Traceroute, active connections (netstat) and routing table viewer/editor
* Packet sniffer: live capture with BPF filters (`tcp port 80`, `icmp`...), decoded
  Ethernet/ARP/IPv4/IPv6/TCP/UDP/ICMP/DNS headers, HTTP request lines, `.pcap` export for
  Wireshark and reading saved captures
* Display available WiFi networks (Windows `netsh`, Linux `nmcli`)
* Works on Windows and Linux
* Interactive menu plus a scriptable command-line mode

---

## Technologies Used

```
Language:
    Java

Libraries:
    java.net
    java.util
    java.io

Concepts:
    - Socket Programming
    - TCP Networking
    - Object-Oriented Programming
    - Command Line Interfaces
```

---

# Project Structure

```
MiniScanner/
│
├── NetworkManager.java
│       └── Application controller, menu and CLI commands
│
├── Menu.java
│       └── Safe input helpers
│
├── Subnet.java
│       └── CIDR parsing and host enumeration
│
├── ReportWriter.java
│       └── JSON/CSV report export
│
├── CommandRunner.java
│       └── Cross-platform wrapper for OS tools
│
├── PacketSniffer.java
│       └── libpcap/Npcap capture via Java's Foreign Function API + header decoding
│
├── Traceroute.java / NetstatViewer.java / RouteManager.java
│       └── Wrappers for tracert/traceroute, netstat, route/ip route
│
├── sandbox/
│       └── Isolated Docker lab for safe testing
│
├── Banner.java
│       └── Terminal ASCII interface
│
├── NetworkInfo.java
│       └── Network interface information
│
├── NetworkScanner.java
│       └── Host discovery module
│
├── PortScanner.java
│       └── TCP port scanning module
│
└── WifiManager.java
        └── WiFi network information module
```

---

# Installation

## Clone the repository

```bash
git clone <your-repository-url>
```

Move into the project directory:

```bash
cd MiniScanner
```

---

# Compile

Compile all Java files:

```bash
javac -d out src/*.java
```

---

# Run

Start the interactive menu:

```bash
java -cp out NetworkManager
```

Or run a single command:

```bash
java -cp out NetworkManager discover 192.168.1.0/24 --report json
java -cp out NetworkManager ports 192.168.1.1 1-1024 --report csv
java -cp out NetworkManager trace example.com
java -cp out NetworkManager help
```

## Packet sniffer

The sniffer calls libpcap directly through Java's Foreign Function & Memory API,
so it needs **Java 22+** but no extra jars. It also needs:

* **Windows:** [Npcap](https://npcap.com) installed, and an Administrator terminal
* **Linux/macOS:** libpcap (`apt install libpcap0.8`), and `sudo`

Add `--enable-native-access=ALL-UNNAMED` to silence the JVM's native-access warning:

```bash
java --enable-native-access=ALL-UNNAMED -cp out NetworkManager sniff --list
java --enable-native-access=ALL-UNNAMED -cp out NetworkManager sniff eth0 --filter "tcp port 80" --count 100 --write reports/web.pcap
java --enable-native-access=ALL-UNNAMED -cp out NetworkManager sniff --read reports/web.pcap
```

Example output:

```
Capturing on eth0 (filter: tcp port 80), 50 packets max, 30s max...

21:03:51.769 TCP  172.30.0.5:59492 -> 172.30.0.10:80 [SYN] ttl=64 len=74
21:03:51.769 TCP  172.30.0.10:80 -> 172.30.0.5:59492 [SYN,ACK] ttl=64 len=74
21:03:51.769 TCP  172.30.0.5:59492 -> 172.30.0.10:80 [ACK] ttl=64 len=66
21:03:51.770 TCP  172.30.0.5:59492 -> 172.30.0.10:80 [PSH,ACK] "GET / HTTP/1.1" ttl=64 len=153
```

Capturing other people's traffic is illegal on networks you don't control. Use the sandbox below.

---

# Testing Safely in the Sandbox

Never test scans on a network you don't own (school, work, public Wi-Fi).
`sandbox/` contains a Docker lab with two isolated networks and decoy services:

```
lab_a 172.30.0.0/24                     lab_b 172.31.0.0/24
scanner  .5                              remote-web  .10  (nginx :80)
web      .10  (nginx :80)                remote-db   .11  (postgres :5432)
cache    .11  (redis :6379)
fake-ssh .12  (SSH banner :22)
fake-ftp .13  (FTP banner :21)
router   .254 <------------------------> router .254
```

Both networks are internal (no route to your LAN or the internet).

```bash
cd sandbox
docker compose up -d                                  # start the lab
docker compose --profile tools run --rm scanner       # interactive menu
docker compose --profile tools run --rm scanner discover --report json
docker compose --profile tools run --rm scanner ports 172.30.0.12 1-1024
docker compose down                                   # tear it down
```

To reach `lab_b`, use menu option 8 (Add Route) with destination `172.31.0.0`,
mask `255.255.255.0`, gateway `172.30.0.254`, then try traceroute to `172.31.0.10`.
Reports are written to `sandbox/reports/`.

To watch the sniffer decode real traffic, open two terminals:

```bash
docker compose --profile tools run --rm --name sniffer scanner sniff eth0 --seconds 60 --write reports/lab.pcap
docker exec sniffer sh -c "ping -c 3 172.30.0.10; wget -qO- http://web/ >/dev/null"
```
After changing the Java code, rebuild with `docker compose --profile tools build scanner`.

---

# Example Usage

When the application starts:

```
==============================
        MiniScanner v1.0
==============================

1. Show Network Information
2. Discover Network Hosts
3. Scan Ports
4. Show Available WiFi Networks
5. Exit

Select option:
```

Example host discovery:

```
--- Host Discovery ---

192.168.1.1 reachable
192.168.1.5 reachable
192.168.1.20 reachable

Hosts found: 3
```

Example port scan:

```
Scanning 192.168.1.1

OPEN PORTS:

22     SSH
80     HTTP
443    HTTPS
3306   MySQL/MariaDB
```

---

# How It Works

## Host Discovery

MiniScanner identifies devices on the local network by checking whether IP addresses respond.

```
Your Computer
      |
      |
      v
192.168.1.0/24 Network

+-------------+
| Router      |
+-------------+

+-------------+
| Laptop      |
+-------------+

+-------------+
| Phone       |
+-------------+
```

---

## Port Scanning

The port scanner attempts TCP connections to determine whether services are available.

```
Target Device

+----------------------+
| Port 22   OPEN       |
| Port 80   OPEN       |
| Port 443  CLOSED     |
+----------------------+

```

---

# Future Improvements

Planned features:

```
[x] Multithreaded scanning
[x] Faster host discovery
[x] Automatic subnet detection
[x] Service banner detection
[ ] OS detection
[x] JSON/CSV report generation
[ ] Graphical User Interface
[x] Better error handling
```

---

# Learning Goals

This project helped explore:

* Java networking APIs
* TCP/IP communication
* Socket programming
* Network discovery concepts
* Software architecture
* Building modular command-line applications

---

# Author

Created as a cybersecurity and Java networking learning project.

```
"Understand the network.
Understand the system.
Build responsibly."
```

# License

This project is for educational purposes.
Use responsibly and only on authorized systems.

```
```
