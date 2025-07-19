use crate::errors::app::AppError;
use axum::{http::StatusCode, response::IntoResponse};

pub async fn ping_handler() -> Result<impl IntoResponse, AppError> {
    tracing::info!("reached ping handler.");
    Ok((StatusCode::OK, "Hello, World!".to_string()).into_response())
}
