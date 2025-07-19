use super::app::AppError;
use axum::http::StatusCode;

#[derive(Debug, thiserror::Error)]
pub enum WindowError {
    #[error(transparent)]
    IoError(#[from] std::io::Error),
    #[error(transparent)]
    AxumError(#[from] axum::Error),
}

impl From<WindowError> for AppError {
    fn from(error: WindowError) -> Self {
        match error {
            WindowError::IoError(e) => AppError {
                status_code: StatusCode::INTERNAL_SERVER_ERROR,
                message: format!("IoError: {e}"),
            },
            WindowError::AxumError(e) => AppError {
                status_code: StatusCode::BAD_REQUEST,
                message: format!("AxumError: {e}"),
            },
        }
    }
}
