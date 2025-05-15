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
mov $100, %r12
mov $40, %r13
add %r13, %r12
mov %r12, %rax
ret
