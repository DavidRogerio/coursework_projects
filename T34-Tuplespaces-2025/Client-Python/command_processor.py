from typing import List
import re
class CommandProcessor:
    SPACE = " "
    BGN_TUPLE = "<"
    END_TUPLE = ">"
    PUT = "put"
    READ = "read"
    TAKE = "take"
    EXIT = "exit"
    GET_TUPLE_SPACES_STATE = "getTupleSpacesState"

    def __init__(self, client_service):
        self.client_service = client_service

    def parse_input(self):
        exit_flag = False
        while not exit_flag:
            try:
                line = input("> ").strip()
                split = line.split(self.SPACE)
                command = split[0]

                if command == self.PUT:
                    self.put(split)
                elif command == self.READ:
                    self.read(split)
                elif command == self.TAKE:
                    self.take(split)
                elif command == self.GET_TUPLE_SPACES_STATE:
                    self.get_tuple_spaces_state()
                elif command == self.EXIT:
                    exit_flag = True
                else:
                    self.print_usage()
            except EOFError:
                break

    def put(self, split: list[str]):
        # Check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]
        delays = [0, 0, 0]

        # Handle optional delays
        if len(split) >= 3:
            valid = True
            for i in range(2, min(len(split), 5)):  # accepts split[2] to split[4]
                if split[i].isdigit():
                    delays[i - 2] = int(split[i])
                else:
                    valid = False
                    break
            if not valid:
                self.print_usage()
                return

        if self.client_service.put(tuple_data, delays):
            print("OK\n")
        else:
            self.print_usage()


    def read(self, split: list[str]):
        # Check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]
        delays = [0, 0, 0]

        if len(split) >= 3:
            valid = True
            for i in range(2, min(len(split), 5)): # min garante que não exploda fora do seu range
                if split[i].isdigit():
                    delays[i - 2] = int(split[i])
                else:
                    valid = False
                    break
            if not valid:
                self.print_usage()
                return

        result = self.client_service.read(tuple_data, delays)
        if result:
            print(f"OK\n{result}\n")
        else:
            self.print_usage()


    def is_concrete_tuple(self, tuple_data: str) -> bool:
        return not any(substr in tuple_data for substr in ['[', ']', '.*', '|'])




    def take(self, split: list[str]):
        # Verifica se o input é válido
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]
        
        # Remove os < > 
        if not tuple_data.startswith("<") or not tuple_data.endswith(">"):
            self.print_usage()
            return

        pattern = tuple_data[1:-1]  # regex sem < >

        # Tenta compilar a regex para verificar se é válida
        try:
            re.compile(pattern)
        except re.error as e:
            print(f"Expressão regular inválida: {e}")
            return

        delays = [0, 0, 0]
        if len(split) >= 3:
            for i in range(2, min(len(split), 5)):
                if split[i].isdigit():
                    delays[i - 2] = int(split[i])
                else:
                    self.print_usage()
                    return

        # Chama o serviço e imprime o resultado
        result = self.client_service.take(tuple_data, delays)
        if result:
            print(f"OK\n{result}\n")


    def get_tuple_spaces_state(self):
        delays = [0, 0, 0]
        result = self.client_service.get_tuple_spaces_state(delays)

        if result is None:
            return

        print("OK")

        if not result:
            print("[]\n")
            return

        print("[", end="")
        for i in range(len(result) - 1):
            print(f"{result[i]}", end=", ")
        print(f"{result[-1]}]\n")


    def print_usage(self):
        print("Usage:\n"
              "- put <element[,more_elements]>\n"
              "- read <element[,more_elements]>\n"
              "- take <element[,more_elements]>\n"
              "- getTupleSpacesState\n"
              "- sleep <integer>\n"
              "- exit\n")

    def input_is_valid(self, input_data: List[str]):
        if (len(input_data) < 2
                or not input_data[1].startswith(self.BGN_TUPLE)
                or not input_data[1].endswith(self.END_TUPLE)):
            return False

        delay_count = len(input_data) - 2
        if delay_count < 0 or delay_count > 3:
            return False

        return True
