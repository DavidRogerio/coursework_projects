import sys
class ClientDebug:
    _debug_flag = False
    # Construtor privado para impedir a criação de instâncias
    def __new__(cls, *args, **kwargs):
        raise NotImplementedError("ClientDebug não pode ser instanciada.")

    @staticmethod
    def setDebugFlag(val: bool):
        """Define o estado da flag de debug."""
        ClientDebug._debug_flag = val

    @staticmethod #ver
    def debug(debugMessage: str):
        """Imprime mensagens de debug se a flag estiver ativada."""
        if ClientDebug._debug_flag:
            print(debugMessage, file=sys.stderr)
