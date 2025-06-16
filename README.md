
PROJECT TITLE: Mulithreaded Chess Game

PURPOSE OF PROJECT: Final Year networking Project

VERSION or DATE: 16/06/2025

HOW TO START THIS PROJECT: 

Running the Project from GitHub
First, you need to download the project files to your computer.

Download the Project: On the main page of the GitHub repository, click the green < > Code button, and then select "Download ZIP".

Unzip the File: Find the downloaded .zip file on your computer and extract its contents. This will create a folder containing all the project files.

Open in BlueJ: Launch the BlueJ application. From the menu, select Project > Open Project... and navigate to the folder you just unzipped. Select the folder to open the project.

Once the project is open in BlueJ, you are ready to start the game.



AUTHORS: Hashir Zahoor Ur Rahman

USER INSTRUCTIONS: 

How to Play: Multithreaded Chess
Welcome to Multithreaded Chess! This is a two-player game designed to be played locally on the same computer. To begin, you will need to run two separate instances of the game application. One player will act as the Host, and the other will be the Client.

Step 1: Start the Game as the Host (Player 1)
The first player must host the game. This creates a game session that the second player can join.

Launch the first instance of the chess game application.

Choose the option to "Host Game" or "Act as Server".

You will be prompted to enter a Port Number. A port is like a digital doorway on your computer. You can choose any number between 1024 and 49151. A good default choice is 8080.

For the server address, use localhost. This tells the game to host on your own machine.

Wait for the second player to connect.

Example Host Setup:

Server Address: localhost

Port: 8080

Step 2: Join the Game as the Client (Player 2)
The second player joins the game session created by the Host.

Launch the second instance of the chess game application.

Choose the option to "Join Game" or "Act as Client".

You will be prompted for the server address and port number. You must use the exact same details that the Host entered.

Enter localhost as the server address.

Enter the same Port Number that the Host chose (e.g., 8080).

Connect to the game. Once the connection is successful, the match will begin!

Gameplay Rules
The game follows standard chess rules.

The game ends when one player achieves checkmate, capturing the opponent's King.

Playing Multiple Games at Once
Because this server is multithreaded, you can host and play multiple, independent chess games on your computer at the same time. To do this, simply have each Host choose a different port number for their game (e.g., Game 1 on port 8080, Game 2 on port 8081, etc.).
