upload.andThen {
  case Success(s) => {
    FileUploadSuccess(item, s)
  }
  case Failure(t) =>
    FileUploadFailure(item, Some(t))
  } pipeTo originalSender
