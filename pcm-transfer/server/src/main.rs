use error::ServerError;
use std::io::Write;
use std::net::{IpAddr, Ipv4Addr, Shutdown, SocketAddr, TcpListener};
use std::sync::{Arc, RwLock};
use std::{thread, time};

pub mod error;

//* Protocol
// 1. create socket
// 2. connect to middle-server
// 4. send stock_data to middle-server
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
                    println!("Client connected: {addr:?}");

                    // read wav file
                    let mut reader = hound::WavReader::open("data/stream.wav")?;
                    // get headers
                    let spec = reader.spec();
                    println!(
                        "WAV: {}Hz, {}ch, bits: {}",
                        spec.sample_rate, spec.channels, spec.bits_per_sample
                    );
                    // get body (PCM samples)
                    let mut samples = reader.samples::<i16>();
                    // define amount of samples
                    // channel == 1 ... monaural
                    // channel == 2 ... stereo
                    let frames_per_chunk = 1024;
                    let samples_per_chunk = frames_per_chunk * spec.channels as usize;
                    // define interval
                    let interval = time::Duration::from_secs_f64(
                        frames_per_chunk as f64 / spec.sample_rate as f64,
                    );

                    // send PCM data to middle-server
                    loop {
                        let mut buf = Vec::with_capacity(samples_per_chunk);

                        // send chunks of a sample
                        for _ in 0..samples_per_chunk {
                            if let Some(Ok(sample)) = samples.next() {
                                buf.extend_from_slice(&sample.to_le_bytes());
                            } else {
                                println!("End of file");
                                break;
                            }
                        }

                        // break point
                        if buf.is_empty() {
                            break;
                        }

                        socket.write_all(&buf)?;
                        println!("Sent {} bytes to client", buf.len());
                        thread::sleep(interval);
                    }
                    // send Over
                    match socket.write_all("Over".as_bytes()) {
                        Ok(_) => {
                            println!("Sent message to client: Over");
                        }
                        Err(e) => {
                            eprintln!("Error writing to socket: {e}");
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
                eprintln!("Error accepting connection: {e}");
                return Err(ServerError::IoError(e));
            }
        }
    }
}
