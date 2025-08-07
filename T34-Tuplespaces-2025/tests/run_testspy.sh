#!/bin/bash

################################################
### PATHS (ajuste conforme necessário)
CLI_PATH="../"
TESTS_FOLDER="${PWD}"
TESTS_OUT_EXPECTED="${TESTS_FOLDER}/expected"
TESTS_OUTPUT="${TESTS_FOLDER}/test-outputs"
VENV_PATH="../.venv" #!!!!!!!!!
################################################
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
################################################

rm -rf "$TESTS_OUTPUT"
mkdir -p "$TESTS_OUTPUT"

# Ativa o ambiente virtual usando o caminho relativo
source "$VENV_PATH/bin/activate" 

# Entra na pasta do cliente Python
cd "$CLI_PATH"

# Ajusta o PYTHONPATH para incluir o diretório atual
export PYTHONPATH=$PYTHONPATH:$(pwd)


i=1
while :
do
    TEST=$(printf "%02d" $i)
    if [ -e "${TESTS_FOLDER}/input${TEST}.txt" ]
    then 
        # Executa o cliente Python a partir da pasta correta (test) e salva a saída
        python3 Client-Python/client_main.py localhost:2001 2 < "${TESTS_FOLDER}/input${TEST}.txt" > "${TESTS_OUTPUT}/out${TEST}.txt"
        
        DIFF=$(diff "${TESTS_OUTPUT}/out${TEST}.txt" "${TESTS_OUT_EXPECTED}/out${TEST}.txt")
        
        if [ "$DIFF" != "" ]
        then
            echo -e "${RED}[${TEST}] TEST FAILED${NC}"
            # Salva as diferenças em diffXX.txt
            echo "$DIFF" > "${TESTS_OUTPUT}/diff${TEST}.txt"
        else
            echo -e "${GREEN}[${TEST}] TEST PASSED${NC}"
        fi
        i=$((i+1))
    else
        break
    fi
done

echo "Check the outputs of each test in ${TESTS_OUTPUT}."
echo "Differences (if any) are saved in ${TESTS_OUTPUT}/diffXX.txt."

