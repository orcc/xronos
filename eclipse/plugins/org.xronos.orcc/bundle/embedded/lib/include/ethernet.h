/**
 * ----------------------------------------------------------------------------
 * __  ___ __ ___  _ __   ___  ___
 * \ \/ / '__/ _ \| '_ \ / _ \/ __|
 *  >  <| | | (_) | | | | (_) \__ \
 * /_/\_\_|  \___/|_| |_|\___/|___/
 * ----------------------------------------------------------------------------
 * Copyright (C) 2015 EPFL SCI STI MM
 *
 * This file is part of XRONOS.
 *
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 *
 */
#ifndef __ETHERNET_H__
#define __ETHERNET_H__

#include "interface.h"
#include <string>

#ifdef WIN32
#include <winsock.h>         // For socket(), connect(), send(), and recv()
#else
#include <sys/types.h>       // For data types
#include <sys/socket.h>      // For socket(), connect(), send(), and recv()
#include <netdb.h>           // For gethostbyname()
#include <arpa/inet.h>       // For inet_addr()
#include <unistd.h>          // For close()
#include <netinet/in.h>      // For sockaddr_in
typedef void raw_type;       // Type used for raw data on this platform
#endif

class Ethernet : public Interface
{
public:
	Ethernet(int, int);
	~Ethernet();

	int recv(void* pBuf, const unsigned uNbVal);
	int send(void* pBuf, const unsigned uNbVal);

protected:
	int m_socket;
};

class UdpServerSocket : public Ethernet
{
public:
	UdpServerSocket(int localPort);
};

class UdpClientSocket : public Ethernet
{
public:
	UdpClientSocket(std::string& localAddr, int localPort);
};

class TcpServerSocket : public Ethernet
{
public:
	TcpServerSocket(int localPort);

};

class TcpClientSocket : public Ethernet
{
public:
	TcpClientSocket(const std::string& localAddr, int localPort);
};

#endif
