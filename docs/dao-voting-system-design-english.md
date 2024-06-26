# LeisureMeta DAO Voting System Design Document

## 1. Project Background and Objective

LeisureMeta is an innovative blockchain platform that supports DAO projects composed of creators and fans. The DAO Voting system designed in this document aims to enable these DAOs to make important decisions, such as fund allocation, in a transparent and democratic manner.

LeisureMeta Chain inherits the advantages of existing DAO systems while providing the following innovative features:

1. High Scalability and Low Cost:
   - Addresses the high gas fees and low throughput issues faced by existing Ethereum-based DAOs.
   - Internal transactions on LeisureMeta Chain are gas-free, enabling frequent voting and decision-making in a cost-effective manner.
2. Integrated Snapshot Functionality:
   - While existing solutions like ERC20Snapshot supported snapshot-based voting, LeisureMeta Chain implements this at the native level.
   - Snapshots can be created and queried consistently for all tokens and NFTs on the chain, enabling voting with various assets without complex smart contracts.
3. Seamless Integration of Diverse Voting Methods:
   - Supports various voting methods such as one person one vote, token-weighted, and NFT-based voting within a single system.
   - Unlike existing solutions that required separate contracts or implementations for each method, LeisureMeta Chain allows easy implementation of all methods through a unified API.
4. Optimized for Creator Economy:
   - Enables closer relationships between creators and fans through governance utilizing NFTs and fan tokens.
   - While existing DAO systems mainly focused on finance or protocol governance, LeisureMeta is specialized in building a new economic ecosystem centered around creators.
5. Enhanced User Experience:
   - All functions including voting, token trading, and NFT minting occur on a single chain, greatly improving user experience.
   - Eliminates the complexity and high entry barriers of existing multi-chain solutions while utilizing the advantages of each function.
6. Flexible Scalability:
   - The modular structure of LeisureMeta Chain allows for easy addition of new voting methods or governance models in the future.
   - This will be a crucial advantage in the rapidly evolving Web3 ecosystem.

This DAO Voting system aims to provide a governance platform where creators and fans can easily and effectively participate by maximizing these advantages of LeisureMeta Chain. While inheriting the strengths of existing solutions, we seek to present a new model of collaboration and value creation in the Web3 era through LeisureMeta's specialized features.

## 2. System Overview

LeisureMeta's DAO Voting system has the following characteristics:

1. A blockchain-based transparent and tamper-proof voting system
2. Voting based on token holdings at a specific point in time using snapshot functionality
3. Support for three voting methods: One person one vote, Fungible Token-based, and NFT-based
4. Flexible voting group settings using Token Definition ID
5. High accessibility due to internal transactions without Gas Fees

## 3. Technical Design

### 3.1 Snapshot Functionality

LeisureMeta's snapshot feature records token holdings at a specific point in time, allowing for balance inquiries regardless of subsequent token movements.

Related APIs:
- `GET /snapshot-state/{definitionID}`: Query token definition snapshot state
- `GET /snapshot-balance/{Account}/{TokenDefinitionID}/{SnapshotID}`: Query token snapshot balance
- `GET /nft-snapshot-balance/{Account}/{TokenDefinitionID}/{SnapshotID}`: Query NFT snapshot balance

### 3.2 DAO Voting System API

#### 3.2.1 Create Vote Proposal

```json
POST /tx

{
  "VotingTx": {
    "CreateVoteProposal": {
      "networkId": 2021,
      "createdAt": "2023-06-21T18:01:00Z",
      "proposalId": "PROPOSAL-2023-001",
      "title": "Community Fund Usage Proposal",
      "description": "Fund allocation for Creator Support Program",
      "votingPower": {
        "LM": 12345
      },
      "voteStart": "2023-06-22T00:00:00Z",
      "voteEnd": "2023-06-29T23:59:59Z",
      "voteType": "TOKEN_WEIGHTED",
      "voteOptions": {
        "1": "Approve",
        "2": "Reject",
        "3": "Abstain"
      },
      "quorum": 1000000, // Minimum participation (e.g., 1,000,000 LM)
      "passThreshold": 0.66 // Approval threshold (66%)
    }
  }
}
```

Example of NFT-based voting:

```json
POST /tx

{
  "VotingTx": {
    "CreateVoteProposal": {
      "networkId": 2021,
      "createdAt": "2023-06-21T18:01:00Z",
      "proposalId": "PROPOSAL-2023-002",
      "title": "Approval for New NFT Collection Launch",
      "description": "Voting for approval of a new NFT collection proposed by the community",
      "votingPower": {
        "NFT-COLLECTION-001": 12347,
        "NFT-COLLECTION-002": 12348
      },
      "voteStart": "2023-06-22T00:00:00Z",
      "voteEnd": "2023-06-29T23:59:59Z",
      "voteType": "NFT_BASED",
      "voteOptions": {
        "1": "Approve",
        "2": "Reject"
      },
      "quorum": 100, // Minimum participation (number of NFTs)
      "passThresholdNumer": 51, // Approval threshold numerator(51%)
      "passThresholdDemon": 100, // Approval threshold denominator(100%)
    }
  }
}
```

#### 3.2.2 Cast Vote

```json
POST /tx

{
  "VotingTx": {
    "CastVote": {
      "networkId": 2021,
      "createdAt": "2023-06-23T10:30:00Z",
      "proposalId": "PROPOSAL-2023-001",
      "selectedOption": "1"
    }
  }
}
```

#### 3.2.3 Tally Votes

```json
POST /tx

{
  "VotingTx": {
    "TallyVotes": {
      "networkId": 2021,
      "createdAt": "2023-06-30T00:01:00Z",
      "proposalId": "PROPOSAL-2023-001"
    }
  }
}
```

#### 3.2.4 Query Vote Proposal

```
GET /vote-proposal/{proposalId}
```

Response:
```json
{
  "proposalId": "PROPOSAL-2023-001",
  "title": "Community Fund Usage Proposal",
  "description": "Fund allocation for Creator Support Program",
  "votingTokens": ["LM"],
  "snapshotId": 12345,
  "voteStart": "2023-06-22T00:00:00Z",
  "voteEnd": "2023-06-29T23:59:59Z",
  "voteOptions": {
    "1": "Approve",
    "2": "Reject",
    "3": "Abstain"
  },
  "quorum": 1000000,
  "passThreshold": 0.66,
  "status": "In Progress",
  "currentResults": {
    "1": 3500000,
    "2": 1200000,
    "3": 300000
  },
  "totalVotes": 5000000
}
```

#### 3.2.5 Query User Voting History

```
GET /vote-history/{account}
```

Response:
```json
[
  {
    "proposalId": "PROPOSAL-2023-001",
    "votedAt": "2023-06-23T10:30:00Z",
    "selectedOption": "1"
  },
  // ... other voting history
]
```

### 3.3 Voting Type Implementation

Voting types are specified through the `voteType` field in `CreateVoteProposal`:

1. One person one vote (`ONE_PERSON_ONE_VOTE`): Implemented to allow only one vote per account
2. Fungible Token-based voting (`TOKEN_WEIGHTED`): Voting rights proportional to LM token holdings
3. NFT-based voting (`NFT_BASED`): Voting rights based on NFT holdings

Implementation for each voting type:

1. One person one vote:
   - The `votingTokens` field is ignored.
   - Each account can cast one vote if it exists at the snapshot point.

2. Fungible Token-based voting (LM token):
   - "LM" is specified in the `votingTokens`.
   - Voting rights are granted based on LM token holdings at the snapshot point.

3. NFT-based voting:
   - The Token Definition ID of the NFT collection is specified in `votingTokens` (e.g., "NFT-COLLECTION-001").
   - Voting rights are granted based on the holdings of the specified NFT collection at the snapshot point.
   - Each NFT has equal weight.

### 3.4 Utilizing Token Definition ID

The `votingTokens` field in `CreateVoteProposal` specifies the tokens used for voting. For TOKEN_WEIGHTED type, LM token is used, and for NFT_BASED type, the Token Definition ID of the relevant NFT collection is used.

### 3.5 Utilizing Snapshot ID

When creating a vote proposal, `snapshotId` is specified to grant voting rights based on token holdings at a specific snapshot point. This prevents the influence of token movements during the voting period and ensures fair voting.

## 4. Security and Transparency

- Transaction signatures: All transactions must be signed with the user's private key.
- Blockchain records: All voting-related transactions are permanently recorded on the blockchain, ensuring transparency.
- Snapshot-based voting: Voting rights are granted based on token holdings at a specific point in time, preventing vote manipulation.
- API security: HTTPS is used to enhance API communication security, and appropriate authentication/authorization mechanisms are implemented.

## 5. Socioeconomic Impact

LeisureMeta's DAO Voting system is expected to have the following socioeconomic impacts:

1. Democratization of the creator economy: Strengthening the relationship between creators and fans through direct participation in decision-making
2. Transparent fund management: Ensuring transparency in fund execution through a blockchain-based voting system
3. Community-driven growth: Encouraging active participation of community members through the DAO structure
4. New utilization of digital assets: Increasing the utility of digital assets through governance participation using NFTs and tokens
5. Decentralized decision-making: Providing opportunities for more stakeholders to participate in decision-making, moving away from centralized power structures

## 6. Implementation Roadmap

1. Implement and test snapshot functionality
   - Build snapshot ID generation and management system
2. Implement DAO Voting system API
   - Implement transaction processing for `CreateVoteProposal`, `CastVote`, `TallyVotes`
   - Implement logic for each voting type
   - Implement APIs for querying vote proposals and history
3. Implement blockchain state management
   - Design state structure for storing vote proposals, participation, and results
4. Develop client application
   - Implement user interface
   - Implement communication logic with API
5. Testing and security audit
6. Beta version release and feedback collection
7. Official version release

## 7. Legal Considerations

This system merely provides a tool for decision-making and does not directly involve fund-raising or management. However, users must comply with relevant regulations in their respective countries, and LeisureMeta will continuously review regulatory compliance through legal consultation.

Key considerations:
1. Data privacy: Protect user information and comply with relevant regulations such as GDPR
2. Securities law: Ensure token-based voting does not violate securities regulations
3. Anti-Money Laundering (AML): Consider introducing KYC procedures if necessary
4. Smart contract audit: Regularly conduct security audits to eliminate vulnerabilities

## 8. Conclusion

LeisureMeta's DAO Voting system provides a fairer and more transparent decision-making platform by utilizing snapshot IDs. By supporting three voting methods (one person one vote, LM token-weighted, and NFT-based), it offers decision-making mechanisms suitable for various situations. This will foster democratic operation and sustainable growth of the LeisureMeta ecosystem.
