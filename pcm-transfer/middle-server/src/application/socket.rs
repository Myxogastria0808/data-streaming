use crate::errors::window::WindowError;
use axum::extract::ws::{Message, Utf8Bytes, WebSocket};
use std::{
    collections::VecDeque,
    net::{IpAddr, Ipv4Addr, SocketAddr},
};
use tokio::{io::AsyncReadExt, net::TcpStream};

// protocol
// 1. create socket
// 2. connect to server
// 4. send binary to client until received "Over"

// Domain
const PORT: u16 = 5000;
// Window Size
const WINDOW_SIZE: usize = 1600;
// Slide Size
const SLIDE_SIZE: usize = 1200;

pub async fn socket(text: Utf8Bytes, socket: &mut WebSocket) -> Result<(), WindowError> {
    // 1.create socket
    let server_address = SocketAddr::new(IpAddr::V4(Ipv4Addr::LOCALHOST), PORT);

    // 2. connect to server
    let mut stream = match TcpStream::connect(server_address).await {
        Ok(stream) => stream,
        Err(e) => {
            eprintln!("Error connecting to server: {e}");
            return Err(WindowError::IoError(e));
        }
    };

    // chunk count
    let mut chunk_count = 0;
    // collect record buffer
    let mut stock_data_buffer: VecDeque<Vec<u8>> = VecDeque::new();

    // 3. send binary to server
    let mut buffer = [0u8; 4096];
    let mut send_buffer: Vec<u8> = Vec::new();
    loop {
        match stream.read(&mut buffer).await {
            Ok(bytes_read) => {
                let buf = &buffer[..bytes_read];

                if chunk_count > WINDOW_SIZE {
                    //* send buffer *//
                    // reset count
                    chunk_count = 0;
                    for _ in 0..SLIDE_SIZE {
                        let chunk = stock_data_buffer.pop_front();
                        if let Some(data) = chunk {
                            send_buffer.extend_from_slice(&data);
                        }
                    }
                    // send data to client
                    (*socket).send(Message::Binary(send_buffer.into())).await?;
                    // reset send buffer
                    send_buffer = Vec::new();
                } else {
                    //* collect buffer *//
                    // count chunk
                    chunk_count += 1;
                    stock_data_buffer.push_back(buf.to_vec());
                }

                // break point
                if buf.windows(b"Over".len()).any(|window| window == b"Over") {
                    break;
                }
            }
            Err(e) => {
                eprintln!("Error receiving message from server: {e}");
                return Err(WindowError::IoError(e));
            }
        }
    }
    Ok(())
}
