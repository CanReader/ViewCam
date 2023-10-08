#ifndef _CLIENT_H_
#define _CLIENT_H_

#include "Utilities.h"

#include <winsock2.h>
#include <WS2tcpip.h>
#include <iostream>
#include <thread>
#include <memory>
#include <sstream>



#define EXPORT extern "C" __declspec(dllexport)


class Client
{
public:
	Client() = delete;
	Client(const char* IP, int PORT);
	~Client();

	void Connect();
	void TryConnect();
	void Disconnect();
	bool IsConnected();

	char* ReceiveData(int& readBytes);
	void SendData(const void* data, size_t size);

private:
	bool CreateSocket();

	const char* m_ip;
	const int m_port;

	bool m_connected;
	SOCKET m_client;
	SOCKET m_server;
	sockaddr_in m_serverAddr;
};

#endif