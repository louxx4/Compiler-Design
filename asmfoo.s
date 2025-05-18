.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
mov $2, %r12d
mov %r12d, %r13d
add $1, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r12d, %r12d
add %r13d, %r12d
mov %r13d, %r13d
add %r12d, %r13d
mov %r13d, %eax
cltq
cqto
mov $256, %r12d
idiv %r12d
mov %edx, %r12d
mov %r12d, %eax
cltq
ret
