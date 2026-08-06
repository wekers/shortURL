for line in (cat .env)
    if test (string trim --left $line) = "" || string match -q "#*" $line
        continue
    end
    set key (string split "=" -- (string split -m 1 "=" -- $line)[1])
    set value (string split -m 1 "=" -- $line)[2]
    set -x $key (string trim --chars='"' -- $value)
end
