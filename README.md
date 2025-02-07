# HeiseiChain-Web

#### Current status
Key Components Covered
- Genesis Block Creation:
The first block in the chain is hardcoded with no previous hash.

- Block Structure:
Blocks include fields like data, hash, previousHash, and timestamp.
A method (calculateHash) ensures the hash is derived from block data.

- Adding Blocks:
Blocks are added to the chain after verifying integrity.
Each new block references the previous block's hash.

- Blockchain Validation:
A method to validate the chain by ensuring:
The hash of each block matches its calculated hash.
The previousHash of each block matches the hash of the preceding block.

- Spring Boot Integration:
Created REST APIs to:
Add blocks (POST /api/blockchain/add)
Retrieve the blockchain (GET /api/blockchain)
Stored the blockchain in-memory (non-persistent).


Donor Creation
http://localhost:8080/api/blockchain/register?username=donor1&role=donor

Volunteer Creation
http://localhost:8080/api/blockchain/register?username=volunteer2&role=volunteer

Camp Creation
http://localhost:8080/api/blockchain/register?username=camp1&role=camp

Transaction Creation
http://localhost:8080/api/blockchain/create?senderUsername=donor1&recipientUsername=volunteer2&value=25&metadata=donatio

