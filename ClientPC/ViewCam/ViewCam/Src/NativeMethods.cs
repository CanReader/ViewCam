using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Runtime.InteropServices;

namespace ViewCam.Src
{
    public class NativeMethods
    {
        [DllImport("ViewCam.Core.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void StartApp();

        [DllImport("ViewCam.Core.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void StopApp();

        [DllImport("ViewCam.Core.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr GetImageBytes(out int length);

        [DllImport("ViewCam.Core.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern bool IsConnected();

        [DllImport("ViewCam.Core.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern Task<IntPtr> GetImageBytesAsync(out int length);


        public byte[] ConvertIntPtrToByteArray(IntPtr ptr, int length)
        {
            byte[] data = new byte[length];
            Marshal.Copy(ptr, data, 0, length);

            return data;
        }
    }
}
