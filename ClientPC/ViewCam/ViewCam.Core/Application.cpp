#include "Application.h"

static const char* IP = "192.168.1.10";
static const int PORT = 8181;

Application* Application::app;

Application* Application::get()
{
	if (app == NULL)
		app = new Application();

	return app;
}

char* Application::GetData()
{
	return data;
}

Application::Application(): client(std::make_unique<Client>(IP,PORT)), data(nullptr)
{
}

Application::~Application()
{
	client.release();
	client = 0;
}

void Application::Start()
{
	WSAData wdata;
	int rc = WSAStartup(MAKEWORD(2, 2), &wdata);

	switch (rc)
	{
	case WSASYSNOTREADY:
		ErrorBox("The underlying network subsystem is not ready for network communication.");
		return;
	case WSAVERNOTSUPPORTED:
		ErrorBox("The version of Windows Sockets support requested is not provided by this particular Windows Sockets implementation.");
		return;
	case WSAEINPROGRESS:
		ErrorBox("A blocking Windows Sockets 1.1 operation is in progress.");
		return;
	case WSAEPROCLIM:
		ErrorBox("A limit on the number of tasks supported by the Windows Sockets implementation has been reached.");
		return;
	case WSAEFAULT:
		ErrorBox("The lpWSAData parameter is not a valid pointer.")
			return;
	default:
		break;
	}

	isRunning = true;
	try
	{
		client->Connect();
		int temp;

		while (isRunning)
		{
			if (!client->IsConnected())
				client->TryConnect();

			try
			{
				data = client->ReceiveData(temp);
			}
			catch (const std::exception& e)
			{
				InfoBox(e.what());
				client->Disconnect();
			}
		}

		WSACleanup();
	}
	catch (const std::exception& e)
	{
		InfoBox(e.what());
	}
}

void Application::Stop()
{
	isRunning = false;
}

EXPORT void StartApp()
{
	Application::get()->Start();
}

void StopApp()
{
	Application::get()->Stop();
}

EXPORT char* GetImageBytes(int& length)
{
	return Application::get()->client->ReceiveData(length);
}

bool IsConnected()
{
	return Application::get()->client->IsConnected();
}
