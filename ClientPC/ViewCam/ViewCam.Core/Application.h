#ifndef _APPLICATION_H_
#define _APPLICATION_H_

#include "Client.h"
#include "Decoder.h"

#pragma comment(lib, "ws2_32.lib")

#ifdef _DEBUG
#define Debug
#elif _RELEASE
#define Release
#endif

EXPORT void StartApp();
EXPORT void StopApp();

EXPORT char* GetImageBytes(int& readtBytes);

EXPORT bool IsConnected();

class Application
{
private:
	Application();
public:
	Application(const Application&) = delete;
	Application(const Application&&) = delete;
	~Application();

	void Start();
	void Stop();
	char* GetData();

	static Application* get();

	std::unique_ptr<Client> client;
private:
	static Application* app;
	bool isRunning;
	char* data;
};


#endif // !_APPLICATION_H_