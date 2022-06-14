use android_logger::Config;
use jni::objects::{JClass, JString};
use jni::JNIEnv;
use log::{trace, Level};
use rustupolis_server::repository::Repository;
use rustupolis_server::server::{Protocol, Server};
use rustupolis_server::server_launcher::ServerLauncher;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "C" fn Java_com_example_rust_1application_MainActivity_main(
    _env: JNIEnv,
    _class: JClass,
    _input: JString,
) {
    android_logger::init_once(Config::default().with_min_level(Level::Trace));

    let ip_address: String =
        _env.get_string(_input).expect("Couldn't get java string!").into();

    let port_tcp = String::from("9000");
    let key = "secret_key_encry";

    let repository = Repository::new("admin");
    repository.add_tuple_space(String::from("GPS_DATA"), vec![String::from("admin")]);

    let server_tcp = Server::new(Protocol::TCP, &ip_address, &port_tcp, &repository,key);

    let server_launcher = ServerLauncher::new(vec![server_tcp]);

    server_launcher.launch_server();
}
