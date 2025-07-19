use crate::application::socket;
use crate::{
    errors::{app::AppError, ws::WebSocketError},
    models::shared_state::RwLockSharedState,
};
use axum::extract::ws::{Message, WebSocket};
use axum::{
    extract::{State, WebSocketUpgrade},
    response::IntoResponse,
};

// handler
pub async fn websocket_handler(
    State(shared_state): State<RwLockSharedState>,
    web_socket: WebSocketUpgrade,
) -> Result<impl IntoResponse, AppError> {
    let shared_state = shared_state.read().await;
    let response = web_socket.on_upgrade(|socket| async move {
        if let Err(error) = websocket_processing(socket).await {
            tracing::error!("WebSocket error: {:?}", error);
        }
    });
    drop(shared_state);
    Ok(response)
}

//websocket
pub async fn websocket_processing(mut socket: WebSocket) -> Result<(), AppError> {
    while let Some(message) = socket.recv().await {
        // Receive a message from the client
        match message {
            Ok(message) => {
                match message {
                    Message::Text(text) => {
                        // received text message
                        println!("Received from client: {text:?}");
                        //* send stat result to client *//
                        socket::socket(text, &mut socket).await?;
                    }
                    Message::Binary(binary) => {
                        tracing::error!("Received binary: {:?}", binary);
                        return Err(WebSocketError::UnexpectedMessageTypeError(
                            "Binary".to_string(),
                        )
                        .into());
                    }
                    Message::Ping(ping) => {
                        tracing::error!("Received ping: {:?}", ping);
                        return Err(
                            WebSocketError::UnexpectedMessageTypeError("Ping".to_string()).into(),
                        );
                    }
                    Message::Pong(pong) => {
                        tracing::error!("Received pong: {:?}", pong);
                        return Err(
                            WebSocketError::UnexpectedMessageTypeError("Pong".to_string()).into(),
                        );
                    }
                    Message::Close(close) => {
                        tracing::info!("Client disconnected: {:?}", close);
                        return Ok(());
                    }
                }
            }
            Err(error) => {
                tracing::error!("Error receiving message: {}", error);
                return Err(WebSocketError::from(error).into());
            }
        }
    }
    Ok(())
}
