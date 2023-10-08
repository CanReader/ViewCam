using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using ViewCam.Src;

namespace ViewCam
{
    public partial class MainWindow : Window
    {
        private string IP = "192.168.1.10";
        private const int port = 8181;

        private TcpClient client;
        private NetworkStream stream;

        Thread Starter;
        Thread Communicator;

        private byte[] ImageByte;

        #region Properties
        private bool _connected = false;
        public bool Connected
        {
            get { return _connected; }
            set
            {
                _connected = value;
                Dispatcher.Invoke(() =>
                {
                    ConnectionStatus.Text = _connected ? "Connected!" : "Not connected!";
                    ConnectionStatus.Foreground = new SolidColorBrush(_connected ? System.Windows.Media.Color.FromRgb(35, 193, 113) : System.Windows.Media.Color.FromRgb(190, 47, 40));
                });
            }
        }
        private int _dropped;
        public int DroppedImages
        {
            get => _dropped;
            set
            {
                _dropped = value;
                Dispatcher.Invoke(() => 
                {
                    ImageDrops.Text = "Image drop: " + value.ToString();
                });
            }
        }
        #endregion

        public MainWindow()
        {
            InitializeComponent();
            Connected = false;
            //client = new TcpClient();

            Starter = new Thread(() =>
            {
                NativeMethods.StartApp();
            });

            Communicator = new Thread(() =>
            {
                Start();
            });
            //ListenServerAsync();
        }

        #region STARTER&STOPPER
        private void Start()
        {
            int readBytes = 0;
            try
            {
                while (true)
                {
                    Connected = NativeMethods.IsConnected();

                    if (!Connected)
                        continue;

                    IntPtr ptr = NativeMethods.GetImageBytes(out readBytes);

                    if (readBytes == 0)
                        continue;

                    ImageByte = new byte[readBytes];

                    Marshal.Copy(ptr,ImageByte,0,readBytes);
                    
                    BitmapImage image = CreateImage(ImageByte, readBytes);

                    //Don't skip if the image is jpeg or not broken!
                    if (image != null)
                    {
                        DisplayImage(CreateImage(ImageByte, readBytes));
                    }
                }
            }
            catch (ThreadAbortException e)
            {}
            catch (Exception e)
            {
                MessageBox.Show(e.Message + "\n" + e.StackTrace, "Error");
            }
        }
        private new void Close()
        {
            if (client != null && stream != null)
            {
                client.Close();
                stream.Close();
            }

            NativeMethods.StopApp();
            Communicator.Abort();
            Starter.Abort();
            Application.Current.Shutdown();
        }
        #endregion

        #region JPEG FUNCTIONS
        private bool IsJpeg(byte[] data)
        {
            if (data.Length < 2)
                return false;

            return data[0] == 0xFF && data[1] == 0xD8;
        }
        private BitmapImage CreateImage(byte[] buffer, int length)
        {
            if (!IsJpeg(buffer))
                return null;

            BitmapImage image = new BitmapImage();

            using (MemoryStream memstream = new MemoryStream(buffer))
            {

                memstream.SetLength(0);
                memstream.Write(buffer, 0, length);
                memstream.Seek(0, SeekOrigin.Begin);

                image.BeginInit();
                image.StreamSource = memstream;
                image.EndInit();
            }

            return image;
        }

        private void DisplayImage(BitmapImage image)
        {
            Application.Current.Dispatcher.Invoke(() =>
            {
                ImageControl.Source = image;
            });
        }
        #endregion

        #region NETWORKING FUNCTIONS
        private async void ListenServerAsync()
        {
            while (true)
            {
                if (!Connected || !client.Connected)
                {
                    await ConnectServerAsync();
                }

                if (stream == null)
                    continue;

                try
                {
                    int defaultSize = 1024000;
                    byte[] buffer = new byte[defaultSize];
                    int readBytes = 0;
                    readBytes = await stream.ReadAsync(buffer, 0, defaultSize);

                    if (readBytes <= 0 || !IsJpeg(buffer))
                    {
                        DroppedImages = DroppedImages + 1;
                        continue;
                    }

                    using (MemoryStream ms = new MemoryStream())
                    {
                        ms.SetLength(0);
                        ms.Write(buffer, 0, readBytes);
                        ms.Seek(0,SeekOrigin.Begin);

                        BitmapImage bit = new BitmapImage();
                        bit.BeginInit();
                        bit.CacheOption = BitmapCacheOption.OnLoad;
                        bit.StreamSource = ms;
                        bit.EndInit();

                        Dispatcher.Invoke(() => 
                        {
                            ImageControl.Source = bit;
                        });
                    }
                }
                catch (SocketException e)
                {
                    Connected = false;
                }
                catch (Exception e)
                {
                    if (client == null || stream == null)
                        return;

                    string source = e.Source.ToString();
                    MessageBox.Show($"Exception: {e.Message}\n {e.StackTrace} ", "ERROR", MessageBoxButton.OK, MessageBoxImage.Error);
                }
            }
        }


        private async Task ConnectServerAsync()
        {
            try
            {
                await client.ConnectAsync(IPAddress.Parse(IP), port);
                stream = client.GetStream();
                ChangeConnectivityThreaded(true);
            }
            catch (SocketException e)
            {
                ChangeConnectivityThreaded(false);
            }
            catch (Exception e)
            {
                if (client == null || stream == null)
                    return;

                MessageBox.Show(e.Message, "Error!", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void ChangeConnectivityThreaded(bool isConnected)
        {
            Connected = isConnected;
        }

        #endregion
        
        #region WINDOW EVENTS
        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            Starter.Start();
            Communicator.Start();
        }

        private void Window_Closed(object sender, EventArgs e)
        {
            Close();
        }
        #endregion
    }
}
