const express = require("express");
const crypto = require("crypto");
const cors = require("cors");
require("dotenv").config();

// Auth URIs
const oidc_auth_uri = "https://oidc.idp.vonage.com/oauth2/auth";
const camara_auth_uri = "https://api-eu-3.vonage.com/oauth2/token";

// Number Verification API
const nv_uri = "https://api-eu.vonage.com/camara/number-verification/v031/verify";

// Network Enablement API
const ne_uri = "https://api-eu.vonage.com/v0.1/network-enablement";

// scope 
const fraud_scope = "dpv:FraudPreventionAndDetection#number-verification-verify-read"

const {
    JWT = "",
    REDIRECT_URI = "http://localhost:3000/callback",
} = process.env

const generateRandomString = (length) =>
  crypto.randomBytes(length).toString("hex").slice(0, length);

const makeFetchRequest = async (url, options) => {
  const response = await fetch(url, options);
  if (!response.ok) {
    const error = await response.text();
    throw new Error(`HTTP Error: ${response.status} - ${error}`);
  }
  return response.json();
};

// App Setup
const app = express();

app.use(express.json());
app.use(cors());

// Routes
app.post("/login", async (req, res) => {
  console.log(req.body)
  const { phone } = req.body || null;
  if (!phone) {
    return res.status(400).json({ error: "Phone number is required." });
  }

  // store phone number to be used in the other route
  app.set('phone', phone);

  const data = {
    phone_number: phone,
    scopes: [fraud_scope],
    state: generateRandomString(20),
  };

  const headers = {
    Authorization: `Bearer ${JWT}`,
    "Content-Type": "application/json",
  };

  try {
    // Network Enablement API call
    const response = await makeFetchRequest(ne_uri, {
      method: "POST",
      headers: headers,
      body: JSON.stringify(data),
    });
    auth_url = { url: response.scopes[fraud_scope].auth_url }
    console.log(auth_url)
    res.json(auth_url);

  } catch (error) {
    console.error("Error when with Network Enablement API:", error.message);
    res.status(500).json({ error: error.message });
  }
});

app.get("/callback", async (req, res) => {
  const { code, state, error: errorDescription } = req.query;

  // phone number has been previously stored
  phone = req.app.get('phone');

  if (!code || !state) {
    return res
      .status(500)
      .json({ error: errorDescription || "Invalid callback request" });
  }

  try {
    // Exchange authorization code for access token
    const tokenResponse = await makeFetchRequest(camara_auth_uri, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Bearer ${JWT}`,
      },
      body: new URLSearchParams({
        code,
        redirect_uri: REDIRECT_URI,
        grant_type: "authorization_code",
      }),
    });

    const { access_token: accessToken } = tokenResponse;

    // Call Number Verification API
    const nvResponse = await makeFetchRequest(nv_uri, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ phoneNumber: phone }),
    });

    res.json(nvResponse);
  } catch (error) {
    console.error("Error during callback processing:", error.message);
    res.status(500).json({ error: "Internal Server Error" });
  }
});

// Start Server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Listening on port ${PORT}`);
});
