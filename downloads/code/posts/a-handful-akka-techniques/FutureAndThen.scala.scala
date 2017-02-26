upload.map { result =>
  FileUploadSuccess(item, s)
} recover { case t =>
    FileUploadFailure(item, Some(t))
} pipeTo originalSender
