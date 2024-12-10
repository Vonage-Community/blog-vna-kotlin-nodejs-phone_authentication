# Node.js OAuth & Number Verification API 

This is a Node.js application that integrates with Vonage Network APIs to:

1. Generate an OAuth auth URL using Vonage's Network Enablement API.
2. Handle the OAuth callback to exchange an authorization code for an access token.
3. Verify a phone number using Vonage's Number Verification API.

## Features

- OAuth 2.0 login with Vonage's Identity Provider.
- Uses `fetch` for making HTTP requests.
- Built with Express.js for handling routes.
- Secure configuration via `.env` file.
- Cross-Origin Resource Sharing (CORS) enabled for API calls.


## Prerequisites

- Node.js (>= 18)

## Installation

1. Install dependencies:
   ```bash
   npm install
   ```

2. Create a `.env` file in the project root and configure the following variables:

   ```env
   JWT=your-jwt-token
   REDIRECT_URI=http://localhost:3000/callback
   ```

## Usage

Start the server:

```bash
node server.js 
```

## Troubleshooting

1. **Missing Environment Variables**  
   Ensure all required variables (`JWT`, and `REDIRECT_URI`) are set in your `.env` file.

2. **Invalid Node.js Version**  
   This app uses the native `fetch` API, available in Node.js 18+. If you're using an older version, install `node-fetch`:
   ```bash
   npm install node-fetch
   ```

3. **OAuth Errors**  
   Double-check your application setup in the [Vonage Dashboard](https://developer.vonage.com/dashboard) and ensure the `redirect URI` matches.

