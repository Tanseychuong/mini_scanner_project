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

* Display network interfaces
* Display local IP addresses
* Discover reachable hosts on a local network
* Scan selected TCP ports
* Display available WiFi networks
* Interactive command-line interface

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
├── Main.java
│       └── Application controller and menu system
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
javac *.java
```

---

# Run

Start MiniScanner:

```bash
java Main
```

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
[ ] Multithreaded scanning
[ ] Faster host discovery
[ ] Automatic subnet detection
[ ] Service banner detection
[ ] OS detection
[ ] JSON/CSV report generation
[ ] Graphical User Interface
[ ] Better error handling
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
