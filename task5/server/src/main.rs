use error::ServerError;
use std::fs::File;
use std::io::{BufRead, BufReader, Write};
use std::net::{IpAddr, Ipv4Addr, Shutdown, SocketAddr, TcpListener};
use std::sync::{Arc, RwLock};
use std::{thread, time};

pub mod error;

//* Protocol
// 1. create socket
// 2. connect to client
// 4. send stock_data to client
// 5. close connection

//* References
// https://zenn.dev/woden/articles/56a452bebb166a
// https://www.coins.tsukuba.ac.jp/~syspro/2024/2024-07-17/index.html
// Socket programmingでは、親サーバがaccept処理のみを行い、
// 子のサーバに処理を渡して、複数threadで処理を行なう

// port
const PORT: u16 = 5000;

fn main() -> Result<(), ServerError> {
    // create shared object
    let data = Arc::new(RwLock::new(0));
    // 1.create socket
    let listener = TcpListener::bind(SocketAddr::new(IpAddr::V4(Ipv4Addr::LOCALHOST), PORT))?;
    println!(
        "Server listening on port: ws://{}:{}",
        Ipv4Addr::LOCALHOST,
        PORT
    );

    // connect
    loop {
        match listener.accept() {
            // connection success!
            Ok((mut socket, addr)) => {
                // clone hared object
                let data = Arc::clone(&data);
                // split into multiple threads
                thread::spawn(move || {
                    // lock shared object (read)
                    let _read = data.read().expect("Failed to acquire read lock");
                    println!("Client connected: {:?}", addr);
                    // send stock_data to client
                    for result in BufReader::new(File::open("stock_data.txt")?)
                        .lines()
                        .skip(1)
                    {
                        match result {
                            Ok(record) => match socket.write_all(format!("{}\n", record).as_bytes()) {
                                Ok(_) => {
                                    println!("Sent message to client: {}", record);
                                }
                                Err(e) => {
                                    eprintln!("Error writing to socket: {}", e);
                                    return Err(ServerError::IoError(e));
                                }
                            },
                            Err(e) => {
                                eprintln!("Error reading record: {}", e);
                                return Err(ServerError::IoError(e));
                            }
                        }
                        // sleep for 48 milliseconds
                        thread::sleep(time::Duration::from_millis(48));
                    }
                    // send Over
                    match socket.write_all("Over\n".as_bytes()) {
                        Ok(_) => {
                            println!("Sent message to client: Over");
                        }
                        Err(e) => {
                            eprintln!("Error writing to socket: {}", e);
                            return Err(ServerError::IoError(e));
                        }
                    }
                    // close connection
                    socket.shutdown(Shutdown::Both)?;
                    Ok(())
                });
            }
            // connection failed
            Err(e) => {
                eprintln!("Error accepting connection: {}", e);
                return Err(ServerError::IoError(e));
            }
        }
    }
}
