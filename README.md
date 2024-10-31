# Anonymous Peer-to-Peer (P2P) File Sharing Program

This project implements a simple peer-to-peer (P2P) file-sharing application with a client-server architecture. It provides functionality similar to uTorrent, but with limited features to maintain user anonymity and improve understanding of P2P file-sharing principles.

## Project Overview

The application consists of a server to manage client connections and communication, as well as clients that interact to share files anonymously. Clients can search for files, download and upload files, and handle file transfer status with pause and resume options.

## Project Specifications

### 1. Server

The server coordinates actions and interactions between clients but does not retain any list of shared files. It primarily handles client requests and ensures anonymity.

#### Server Requirements:
- **Client Anonymity**: Clients are only known by their usernames.
- **No File Storage**: The server does not store lists of shared files.
- **Request Handling**: Handles all client requests including connection, disconnection, search requests, and file transfer coordination.
- **Client Introduction**: Facilitates initial communication between clients for file transfer without storing an online user list.
- **Username Duplication**: Manages duplicate usernames upon client connection.

### 2. Client

The client includes a minimal GUI interface for user interaction. Clients can search for files, initiate downloads, upload files, and see progress indicators.

#### Client Requirements:
- **Connection to Server**: Supports multiple clients connecting to a single server.
- **Anonymity**: No display of an online user list to preserve anonymity.
- **Unique Username**: Clients select a unique username upon connection.
- **Uninterrupted Service**: Client connections/disconnections do not affect server stability or other clients.
- **File Search**: Clients can search for files, returning both exact matches and substring matches (handled by the server).
- **Indirect Search**: Search requests are sent through the server; no broadcasting of requests.
- **Progress Indicator**: Displays download/upload progress
- **Download Pause/Resume**: Provides pause and resume functions for downloads.
- **Single Stream Handling**: Supports one download and one upload at a time by using different ports for each.

### 3. Interaction

The file download process includes server coordination and message-key verification for secure connection setup between clients.

#### Download Process:
1. **Initiate Request**: The client sends a download request with a randomly generated message-key.
2. **Server Forwarding**: The server forwards the originating client's address, request, and message-key to the target client.
3. **Target Response**: If the target client accepts, it contacts the requesting client with the received address and message-key.
4. **Verification**: The originating client verifies the message-key before establishing a download stream.

