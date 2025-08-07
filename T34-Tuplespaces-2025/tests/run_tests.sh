################################################
### PATHS (feel free to tweak paths accordingly)
CLI_PATH=${PWD}/../Client-Java
TESTS_FOLDER=${PWD}
TESTS_OUT_EXPECTED=${TESTS_FOLDER}/expected
TESTS_OUTPUT=${TESTS_FOLDER}/test-outputs
################################################
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
################################################

# Limpa e recria a pasta de outputs de teste
rm -rf "$TESTS_OUTPUT"
mkdir -p "$TESTS_OUTPUT"

cd "$CLI_PATH"

i=1
while :
do
    TEST=$(printf "%02d" $i)
    # Verifica se existe um ficheiro de input correspondente
    if [ -e "${TESTS_FOLDER}/input${TEST}.txt" ]
    then 
        # Executa o cliente e salva a saída
        mvn --quiet exec:java < "${TESTS_FOLDER}/input${TEST}.txt" > "${TESTS_OUTPUT}/out${TEST}.txt"
        
        # Compara com a saída esperada
        DIFF=$(diff "${TESTS_OUTPUT}/out${TEST}.txt" "${TESTS_OUT_EXPECTED}/out${TEST}.txt")
        
        if [ "$DIFF" != "" ]
        then
            echo -e "${RED}[${TEST}] TEST FAILED${NC}"
            # Salva as diferenças num ficheiro diffXX.txt
            echo "$DIFF" > "${TESTS_OUTPUT}/diff${TEST}.txt"
        else
            echo -e "${GREEN}[${TEST}] TEST PASSED${NC}"
        fi
        # Avança para o próximo teste (pulando 4)
        i=$((i+1))
    else
        # Sai do loop se não houver mais ficheiros de input
        break
    fi
done

echo "Check the outputs of each test in ${TESTS_OUTPUT}."
echo "Differences (if any) are saved in ${TESTS_OUTPUT}/diffXX.txt."

