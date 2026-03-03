(ns domain.token-verifier)

(defprotocol TokenVerifier
  (verify-provider-token [verifier provider id-token]
    "Verifies an OAuth id-token for the given provider.
    Returns {:subject-id <string> :email <string> :name <string>}.
    Throws on invalid token."))
