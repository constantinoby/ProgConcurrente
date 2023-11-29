--Autor: Constantino Byelov Serdiuk

with Ada.Text_IO; use Ada.Text_IO;
with Ada.Numerics.Discrete_Random;

procedure p2 is

    -- Declaraciones
    --Funciones de generado de numeros random
    function randNum return Integer is
        type randRange is new Integer range 1 .. 10;
        package Rand_Int is new Ada.Numerics.Discrete_Random (randRange);
        use Rand_Int;
        gen : Generator;
        num : randRange;
    begin
        Reset (gen);
        num := Random (gen);
        return Integer (num);
    end randNum;

    function randNBig return Integer is
        type randRange is new Integer range 4 .. 6;
        package Rand_Int is new Ada.Numerics.Discrete_Random (randRange);
        use Rand_Int;
        gen : Generator;
        num : randRange;
    begin
        Reset (gen);
        num := Random (gen);
        return Integer (num);
    end randNBig;

    --Objeto protegido Puente
    package def_puente is

        --Declaraciones del objeto protegido Puente
        protected type Puente is
            procedure vehiculoLock (id : Integer; direccion : Character);
            entry carNorthEntry (id : Integer; direccion : Character);
            entry carSouthEntry (id : Integer; direccion : Character);
            procedure carLeave (id : Integer; direccion : Character);
            entry ambulanciaEntry (id : Integer);
            procedure ambulanciaLeave (id : Integer);
        private
            SouthWaiting : Integer :=
               0;                --Contador vehiculos esperando en el sur
            NorthWaiting : Integer :=
               0;              --Contador de vehiculos esperando en el norte
            enProceso    : Boolean :=
               False;              --Variable que controla si hay vehículos utilizando el puente
            ambulancia   : Boolean :=
               False;           --Variable que controla si hay una ambulancia esperando
        end Puente;

    end def_puente;

    package body def_puente is

        --Cuerpo del objeto protegido Puente
        protected body Puente is

            --Función responsable de gestionar los vehiculos esperando
            procedure vehiculoLock (id : Integer; direccion : Character) is
            begin
                --Si se trata de la ambulancia avisamos de que está esperando poniendo la variable booleana "ambulancia" a true
                if id = 112 then
                    Put_Line
                       ("+++++Ambulància " & Integer'Image (id) &
                        " espera per entrar");
                    ambulancia := True;
                    --Si se trata de un coche aumentamos el contador de espera correspondiente dependiendo de su dirección
                else
                    if direccion = 'S' then
                        SouthWaiting := SouthWaiting + 1;
                        Put_Line
                           ("El cotxe " & Integer'Image (id) &
                            " espera a l'entrada SUD, esperen al SUD: " &
                            Integer'Image (SouthWaiting));
                    else
                        NorthWaiting := NorthWaiting + 1;
                        Put_Line
                           ("El cotxe " & Integer'Image (id) &
                            " espera a l'entrada NORD, esperen al NORD: " &
                            Integer'Image (NorthWaiting));
                    end if;
                end if;
            end vehiculoLock;

            -- Función para que los coches que están esperando en el norte pasen el puente
            -- Hay dos casos: Primero que no haya una ambulancia esperando
            -- En segundo lugar el numero de coches esperando en el norte debe ser mayor o igual al número de coches esperando en el sur
            -- en ese caso el coche podrá pasar, poniendo la variable "enProceso" a true y disminuyendo el contador del norte
            entry carNorthEntry (id : Integer; direccion : Character)
               when not enProceso and not ambulancia and
               NorthWaiting >= SouthWaiting
            is
            begin
                enProceso    := True;
                NorthWaiting := NorthWaiting - 1;
                Put_Line
                   ("El cotxe " & Integer'Image (id) &
                    " entra al pont. Esperen al NORD " &
                    Integer'Image (NorthWaiting));
            end carNorthEntry;

            -- Función para que los coches que están esperando en el sur pasen el puente
            -- Hay dos casos: Primero que no haya una ambulancia esperando
            -- En segundo lugar el numero de coches esperando en el sur debe ser mayor al número de coches esperando en el norte
            entry carSouthEntry (id : Integer; direccion : Character)
               when not enProceso and not ambulancia and
               SouthWaiting > NorthWaiting
            is
            begin
                enProceso    := True;
                SouthWaiting := SouthWaiting - 1;
                Put_Line
                   ("El cotxe " & Integer'Image (id) &
                    " entra al pont. Esperen al SUD " &
                    Integer'Image (SouthWaiting));
            end carSouthEntry;

            --Función para que el coche salga del puente
            procedure carLeave (id : Integer; direccion : Character) is
            begin
                Put_Line
                   ("------> El vehicle " & Integer'Image (id) &
                    " surt del pont");
                enProceso := False;
            end carLeave;

            --Función para que la ambulancia pase por el puente
            --Aquí la única condición para que entre será que no haya ningún vehículo enProceso, ya que la ambulancia tiene prioridad de paso
            entry ambulanciaEntry (id : Integer) when not enProceso is
            begin
                enProceso  := True;
                ambulancia := False;
                Put_Line
                   ("+++++Ambulància " & Integer'Image (id) & " és al pont");
            end ambulanciaEntry;

            --Función para que la ambulancia salga del puente
            procedure ambulanciaLeave (id : Integer) is
            begin
                Put_Line
                   ("------> El vehicle " & Integer'Image (id) &
                    " surt del pont");
                enProceso := False;
            end ambulanciaLeave;

        end Puente;

    end def_puente;

    --Inicialización del objeto protegido puente
    ProcPuente : def_puente.Puente;

    --Task Vehiculo
    task type Vehiculo (id : Integer; direccion : Character);

    --Cuerpo de Vehiculo
    task body Vehiculo is
    begin

        --Si es la ambulancia
        if id = 112 then
            --Tiempo random para que la ambulancia empiece a moverse
            delay Duration (randNum);
            Put_Line ("L'ambulància " & Integer'Image (id) & " està en ruta");
            --Tiempo random de llegada a la entrada del puente
            delay Duration (randNBig);
            --Avisamos que la ambulancia esta esperando
            ProcPuente.vehiculoLock (id, direccion);
            --Hacemos que la ambulancia pase el puente
            ProcPuente.ambulanciaEntry (id);
            --Simulación del tiempo que tarda en pasar
            delay Duration (randNBig);
            --La ambulancia sale del puente
            ProcPuente.ambulanciaLeave (id);

        else -- Si es otro vehiculo
            --Tiempo random para que el vehiculo empiece a moverse
            delay Duration (randNum);
            Put_Line
               ("El cotxe " & Integer'Image (id) &
                " està en ruta en direcció " & Character'Image (direccion));

            --Tiempo random de llegada a la entrada del puente
            delay Duration (randNum);
            --El coche llega al puente y espera, aumentando el contador que correspondiente
            ProcPuente.vehiculoLock (id, direccion);
            --El coche entra en el puente y reduce el contador correspondiente
            if direccion = 'N' then
                ProcPuente.carNorthEntry (id, direccion);
            else
                ProcPuente.carSouthEntry (id, direccion);
            end if;
            --Tiempo random de paso por el puente
            delay Duration (randNBig);
            --El coche sale del puente
            ProcPuente.carLeave (id, direccion);

        end if;

    end Vehiculo;

    --Inicialización de los vehiculos de la simulacion
    c1         : Vehiculo (1, 'S');
    c2         : Vehiculo (2, 'N');
    c3         : Vehiculo (3, 'S');
    c4         : Vehiculo (4, 'N');
    c5         : Vehiculo (5, 'S');
    ambulancia : Vehiculo (112, 'N');

begin

    null;

end p2;
