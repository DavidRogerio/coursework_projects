import sys
from typing import List
from client_service import ClientService
from command_processor import CommandProcessor
from client_debug import ClientDebug
from client_debug import ClientDebug


class ClientMain:
    @staticmethod
    def main(args: List[str]):
        print("ClientMain\n")

        if len(args) == 3 and args[2] == "-debug":
            ClientDebug._debug_flag = True  # Ativa a flag de debug

        # receive and print arguments
        ClientDebug.debug(f"Received {len(args)} arguments")
        for i, arg in enumerate(args):
            ClientDebug.debug(f"arg[{i}] = {arg}\n")

        # check arguments
        if len(args) < 2:
            ClientDebug.debug("Argument(s) missing!", file=sys.stderr)
            ClientDebug.debug("Usage: python3 client_main.py <server.host>:<server.port> <client_id> [-debug]", file=sys.stderr)
            return

        

        host_port = args[0]
        client_id = args[1]


        service = ClientService(host_port, client_id)
        parser = CommandProcessor(service)
        parser.parse_input()
        service.shutdown()

if __name__ == "__main__":
    ClientMain.main(sys.argv[1:])