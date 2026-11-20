This PR adds a basic SECURITY.md file to establish a formal vulnerability reporting process.

Currently, this repository does not have GitHub's Private Vulnerability Reporting enabled, nor does it have a security policy file.

During a security review, I identified three medium-severity vulnerabilities in Glide:

Denial of Service via GIF Out-of-Bounds Array Access
Denial of Service via Integer Overflow in Buffer Allocation
Sensitive Header Leakage via HTTP Redirects
Because there is no private reporting channel open, I cannot securely disclose the full vulnerability report and Proof of Concept (PoC) payloads here in public.

Motivation and Context
Maintainers: Please accept this PR to establish a security policy, and please contact me or establish a private GitHub Security Advisory draft so I can share the complete professional report and PoCs with you privately.

These vulnerabilities affect the GIF decoder and HTTP fetcher in Glide up to version 4.16.0.

