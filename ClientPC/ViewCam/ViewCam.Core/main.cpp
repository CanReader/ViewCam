#include <winsock2.h>
#include <WS2tcpip.h>
#include <iostream>
#include <fstream>
#include <filesystem>

#pragma comment(lib, "ws2_32.lib")

#define println(exp) std::cout << exp << "\n";

SOCKET client;
SOCKET server;

bool CreateSocket();
char* GetImageBuffer();
void SaveImageFile(char* Path);
int IsExistInDirectory(const char* Directory);

int main()
{
	if (!CreateSocket())
		return -1;

	while (true)
	{
		std::cout << "The image data is: " << GetImageBuffer() << "\n";
	}

	closesocket(client);
	WSACleanup();

	return 1;
}

bool CreateSocket()
{
	WSAData data;
	int err = WSAStartup(MAKEWORD(2, 2), &data);

	if (err == -1)
	{
		println("Failed to start the API");
		return false;
	}


	client = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (client == INVALID_SOCKET) {
		std::cerr << "Socket creation failed with error: " << WSAGetLastError() << std::endl;
		WSACleanup();
		return false;
	}

	sockaddr_in serverAddr;
	serverAddr.sin_family = AF_INET;
	serverAddr.sin_port = htons(8181);
	err = InetPtonA(AF_INET, "192.168.1.5", &(serverAddr.sin_addr));
	if (err != 1)
	{
		WSACleanup();
		closesocket(client);
		return false;
	}

	server = connect(client, (sockaddr*)&serverAddr, sizeof(serverAddr));
	if (server == -1) {
		std::cerr << "Connect failed with error: " << WSAGetLastError() << std::endl;
		closesocket(client);
		WSACleanup();
		return false;
	}

	println("The client has been connected to the server!!!");

	return true;
}

char* GetImageBuffer()
{
	const int BufferSize = 1024;
	char buffer[BufferSize];

	if (recv(client, buffer, sizeof(buffer), 0) == -1)
		return (char*)"Failed to get image data! ";

	return buffer;
}
