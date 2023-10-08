#ifndef _UTILITIES_H_
#define _UTILITIES_H_

#define Println(TEXT) std::cout << TEXT << "\n";
#define PrintInfo(TEXT) Println("INFO: " << TEXT);
#define PrintWarning(TEXT) Println("WARNING: " << TEXT);
#define PrintError(TEXT) Println("ERROR: " << TEXT);
#define PrintFatal(TEXT) Println("FATAL: " << TEXT);


#define InfoBox(TEXT) MessageBoxA(NULL, TEXT, "INFO", MB_OK); PrintInfo(TEXT);
#define ErrorBox(TEXT) MessageBoxA(NULL, TEXT, "ERROR", MB_OK); PrintError(TEXT);


#endif // !_UTILITIES_H_