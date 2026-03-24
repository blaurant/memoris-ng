(ns domain.document-signer)

(defprotocol DocumentSigner
  (create-html-signature-request [signer html email]
    "Create a signature request from HTML content.
     Returns {:submission-id int :signing-url string}.")
  (submission-completed? [signer submission-id]
    "Check if a submission has been completed. Returns true/false.")
  (get-signed-document-url [signer submission-id]
    "Retrieve the URL of the signed PDF for a completed submission.
     Returns a string URL or nil if not yet signed."))
