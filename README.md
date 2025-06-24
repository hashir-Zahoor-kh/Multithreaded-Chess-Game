Multithreaded Chess Game in Java

Overview

This project is a networked, two-player chess game implemented in Java. It supports up to 20 concurrent matches using a multithreaded server-client architecture.

Key Features

* JavaFX-based graphical user interface for real-time interaction
* Server pairs players and manages state for multiple games simultaneously
* Game engine validates all legal chess moves using FEN-based state checking
* Includes support for special moves such as castling, pawn promotion, and en passant
* Uses UCI-style input format for structured move handling

Technologies Used

* Java and JavaFX
* TCP/IP sockets
* Multithreading with Java's Thread class
* Object serialization for communication

Architecture

* Server component: Handles matchmaking, threads, and shared game state
* Client component: Provides JavaFX-based interface and communicates with server
* Game state engine: Interprets and validates FEN-based board positions and enforces rules

How to Run

1. Clone the repository:

   ```bash
   git clone https://github.com/hashir-Zahoor-kh/Multithreaded-Chess-Game.git
   ```

2. Compile and run the server:

   ```bash
   javac server/Server.java
   java server.Server
   ```

3. Compile and run the client (open two terminals for a match):

   ```bash
   javac client/Client.java
   java client.Client
   ```

> Ensure both clients and server run on the same machine or within the same local network for proper communication.

Folder Structure


/server     → Handles socket connections, match pairing, and threads
/client     → JavaFX user interface and game communication
/GameState  → Core logic for board state and move validation


License

 Copyright 2025 Hashir Zahoor Ur Rahman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

