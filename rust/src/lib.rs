use android_logger::Config;
use jni::JNIEnv;
use jni::objects::{JClass, JObject};
use log::{Level, trace};
use rustupolis_server::repository::Repository;
use rustupolis_server::server::{Protocol, Server};
use rustupolis_server::server_launcher::ServerLauncher;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_example_rust_1application_MainActivity_main(    _env: JNIEnv,
                                                                           _class: JClass,
                                                                           _callback: JObject) {
    android_logger::init_once(Config::default().with_min_level(Level::Trace));

    let ip_address = String::from("192.168.1.139");
    let port_tcp = String::from("9000");
    let port_udp = String::from("9001");

    let repository = Repository::new("admin");
    repository.add_tuple_space(String::from("GPS_DATA"),vec![String::from("admin")]);

    let server_tcp = Server::new(Protocol::TCP, &ip_address, &port_tcp, &repository);
    let server_udp = Server::new(Protocol::UDP, &ip_address, &port_udp, &repository);

    let server_launcher = ServerLauncher::new(vec![server_tcp, server_udp]);
    server_launcher.launch_server();
}