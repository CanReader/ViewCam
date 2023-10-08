#include "Client.h"

Client::Client(const char* IP, int PORT) : 
	m_connected(false),
	m_ip(IP),
	m_port(PORT),
	m_client(INVALID_SOCKET),
	m_server(INVALID_SOCKET),
	m_serverAddr(sockaddr_in())
{
	CreateSocket();
}

Client::~Client()
{
	closesocket(m_client);
}


void Client::Connect()
{
	if (m_client == INVALID_SOCKET)
		if (!CreateSocket())
			return;

	m_server = connect(m_client,(sockaddr*)&m_serverAddr,sizeof(m_serverAddr));
	
	if (m_server == -1)
	{
		std::string str = "Failed to connect server! Error code: " + std::to_string(WSAGetLastError());
		InfoBox(str.c_str());
		closesocket(m_client);
		return;
	}
	else
		PrintInfo("The client has been connected to the server!");

	m_connected = true;
}

void Client::TryConnect()
{
	if (m_client == INVALID_SOCKET)
		if (!CreateSocket())
			return;

	if (m_connected)
		return;

	m_server = connect(m_client, (SOCKADDR*)&m_serverAddr, sizeof(m_serverAddr));

	if (m_server == SOCKET_ERROR)
	{
		PrintError("Reconnection has been failed!");
		return;
	}
	else
		PrintInfo("The client has been connected to the server!");

	m_connected = true;
}

void Client::Disconnect()
{
	if (m_client != INVALID_SOCKET)
	{
		closesocket(m_client);
		m_client = INVALID_SOCKET;
		std::cout << "INFO: The client has been disconnected from the server.\n";
	}
	else 
		PrintWarning("The client already disconnected.");

	m_connected = false;
}

bool Client::IsConnected()
{
	return m_connected;
}

char* Client::ReceiveData(int& readBytes)
{
	const int BufferSize = 102400;
	int readByte = 0;
	char buffer[BufferSize];

	if ((readByte = recv(m_client, buffer, BufferSize, 0)) <= 0)
	{
		Disconnect();
		PrintError("Failed to receive data from the server!" << WSAGetLastError());
	}

	readBytes = readByte;

	PrintInfo(std::to_string(readByte).c_str());

	return buffer;
}

bool Client::CreateSocket()
{
	m_client = socket(AF_INET, SOCK_STREAM, NULL);
	
	m_serverAddr.sin_family = AF_INET;
	m_serverAddr.sin_port = htons(m_port);
	int rc = InetPtonA(AF_INET, m_ip, &(m_serverAddr.sin_addr.S_un.S_addr));

	if (rc != 1)
	{
		closesocket(m_client);
		PrintFatal("Failed to cast IP adress! Please check your code..." << WSAGetLastError());
		return false;
	}

	return true;
}
