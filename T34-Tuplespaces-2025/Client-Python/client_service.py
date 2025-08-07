import sys
import grpc
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')

from TupleSpaces_pb2 import PutRequest, ReadRequest, TakeRequest, getTupleSpacesStateRequest
from TupleSpaces_pb2_grpc import TupleSpacesStub
from client_debug import ClientDebug

class ClientService:
    # Atributos da classe
    def __init__(self, host_port: str, client_id: int):
        self.client_id = client_id
        self.channel = grpc.insecure_channel(host_port)
        self.stub = TupleSpacesStub(self.channel)

    def put(self, tuple: str, delays: list[int]):
        ClientDebug.debug(f"Client {self.client_id}: put({tuple}) delays={delays}")
        try:
            request = PutRequest(newTuple=tuple)
            metadata = [("client-id", str(self.client_id))] + [(f"delay{i+1}", str(delay)) for i, delay in enumerate(delays)]
            response = self.stub.put(request, metadata=metadata)
            return True
        except grpc.RpcError as e:
            status_code = e.code()
            ClientDebug.debug(f"Error: CODE {status_code}")
            return False

    def read(self, pattern: str, delays: list[int]):
        ClientDebug.debug(f"Client {self.client_id}: read({pattern}) delays={delays}")
        try:
            request = ReadRequest(searchPattern=pattern)
            metadata = [("client-id", str(self.client_id))] + [(f"delay{i+1}", str(delay)) for i, delay in enumerate(delays)]
            response = self.stub.read(request, metadata=metadata)
            return response.result
        except grpc.RpcError as e:
            status_code = e.code()
            ClientDebug.debug(f"Error: CODE {status_code}")
            return ""

    def take(self, pattern: str, delays: list[int]):
        ClientDebug.debug(f"Client {self.client_id}: take({pattern}) delays={delays}")
        try:
            request = TakeRequest(searchPattern=pattern)
            metadata = [("client-id", str(self.client_id))] + [(f"delay{i+1}", str(delay)) for i, delay in enumerate(delays)]
            response = self.stub.take(request, metadata=metadata)
            return response.result
        except grpc.RpcError as e:
            status_code = e.code()
            ClientDebug.debug(f"Error: CODE {status_code}")
            return ""

    def get_tuple_spaces_state(self, delays: list[int]):
        ClientDebug.debug(f"Client {self.client_id}: getTupleSpacesState() delays={delays}")
        try:
            request = getTupleSpacesStateRequest()
            metadata = [("client-id", str(self.client_id))] +  [(f"delay{i+1}", str(delay)) for i, delay in enumerate(delays)]
            response = self.stub.getTupleSpacesState(request, metadata=metadata)
            return response.tuple
        except grpc.RpcError as e:
            status_code = e.code()
            ClientDebug.debug(f"Error: CODE {status_code}")
            return None


    def shutdown(self):
        self.channel.close()
