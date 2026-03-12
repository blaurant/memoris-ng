(ns domain.password-hasher)

(defprotocol PasswordHasher
  (hash-password [hasher raw-password] "Hash a raw password.")
  (check-password [hasher raw-password hashed] "Check a raw password against a hash. Returns boolean."))
