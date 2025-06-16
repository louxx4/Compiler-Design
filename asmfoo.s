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
_basic_0:
mov $1, %r12d
sar $1, %r12d
mov $1, %eax
cltq
cqto
idiv %r12d
mov %eax, %r13d
mov %r13d, %r12d
jmp _basic_1
_basic_1:
mov %r12d, %eax
cltq
ret
