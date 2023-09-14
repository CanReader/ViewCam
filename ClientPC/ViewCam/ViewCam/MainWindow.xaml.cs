using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Runtime.Remoting;
using System.Security.AccessControl;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;                                        
using System.Windows.Shapes;

namespace ViewCam
{
    public partial class MainWindow : Window
    {
        private string IP = "192.168.1.10";
        private const int port = 8181;

        private TcpClient client;
        private NetworkStream stream;

        private bool _connected = false;
        public bool Connected
        {
            get
            {
                return _connected;
            }
            set 
            {
                _connected = value;

                if (client != null && !client.Connected)
                    _connected = false;

                if (_connected)
                {
                    ConnectionStatus.Text = "Connected!";
                    ConnectionStatus.Foreground = new SolidColorBrush(System.Windows.Media.Color.FromRgb(35, 193, 113));
                }
                else
                { 
                    ConnectionStatus.Text = "Not connected!";
                    ConnectionStatus.Foreground = new SolidColorBrush(System.Windows.Media.Color.FromRgb(190, 47, 40));
                }
            }
        }
        private bool connecting = true;

        public MainWindow()
        {
            InitializeComponent();

            Connected = false;
            client = new TcpClient();

            Thread t = new Thread(ListenServer);
            t.Start();

        }

        /* The fucking algorithm:
         * 
         * Initialize UI components,
         * Start a thread for listening the server
         * Listen for the server
         * 
         * 
         * **/

        private void ListenServer()
        {
            while (true)
            {
                if (!Connected)
                    ConnectServer();
                
                try
                {

                    if (client != null || !client.Connected || stream == null || !stream.CanRead)
                        continue;

                    byte[] data = new byte[20240];
                    int readBytes = stream.Read(data, 0, 20240);

                    if (readBytes <= 0)
                        continue;

                    using (MemoryStream memStream = new MemoryStream())
                    {
                        memStream.Write(data,0,readBytes);

                        BitmapImage bit = new BitmapImage();
                        bit.BeginInit();
                        bit.CacheOption = BitmapCacheOption.OnLoad;
                        bit.StreamSource = memStream;
                        bit.EndInit();

                        Dispatcher.Invoke(new Action(() =>
                        {
                            ImageControl.Source = bit;
                        }));


                    }

                }
                catch (Exception e)
                {
                    MessageBox.Show(e.Message,"Error!",MessageBoxButton.OK,MessageBoxImage.Error);
                    break;
                }
            }
        }

        private void ConnectServer()
        {
            try
            {
                client.Connect(IPAddress.Parse(IP), port);
                stream = client.GetStream();

                Dispatcher.Invoke((Action)(() =>
                { 
                    Connected = true;
                }));
            }
            catch (SocketException e)
            {
                return;
            }
            catch (Exception e)
            {
                MessageBox.Show(e.Message,"Error!",MessageBoxButton.OK,MessageBoxImage.Error);
            }
            
        }

        private void ReceiveImage()
        {
            
        }

        private void DisplayImage(Bitmap bitmap)
        {
            
        }

        private void Close()
        {
            if (client != null && client.Connected)
            {
                client.Close();
                stream.Close();
            }

            Connected = false;
        }

        private void Window_Closed(object sender, EventArgs e)
        {
            Close();
        }
    }
}