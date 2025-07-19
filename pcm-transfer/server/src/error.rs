#[derive(Debug, thiserror::Error)]
pub enum ServerError {
    #[error(transparent)]
    IoError(#[from] std::io::Error),
    #[error(transparent)]
    HoundError(#[from] hound::Error),
    #[error("SamplesNotFoundError: No samples found in the WAV file.")]
    SamplesNotFoundError,
}
