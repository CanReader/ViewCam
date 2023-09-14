using System;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;

namespace ViewCam.Src
{
    public class Client
    {
        public string IP;
        public static readonly int PORT = 8181;

        private Socket clientSocket;

        public Client() 
        {
            clientSocket = new Socket(SocketType.Stream,ProtocolType.IPv4);
            clientSocket.Connect(IP,PORT);
        }
    }
}
